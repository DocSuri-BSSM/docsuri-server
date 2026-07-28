# AWS 배포 가이드

EC2 인스턴스 한 대에 Docker Compose로 `app`(Spring Boot) + `db`(PostgreSQL)를 띄우는 가장 단순한 구성 기준이다. 더 큰 구성(ECS, RDS 분리 등)으로 갈 때도 아래 "운영 시 유의할 점"의 전제(단일 인스턴스, 로컬 파일 저장)는 먼저 읽어둘 것.

## 0. 준비물

- AWS 계정, EC2에 SSH 접속할 키페어
- Gemini API 키 (`GEMINI_API_KEY`)
- (선택) 도메인 — HTTPS를 붙이려면 필요

## 1. EC2 인스턴스 생성

| 항목 | 권장값 | 이유 |
|---|---|---|
| AMI | Amazon Linux 2023 또는 Ubuntu 22.04 | 아래 설치 명령이 이 둘 기준 |
| 인스턴스 타입 | **t3.small 이상** | 인스턴스 안에서 `docker compose up --build`가 Gradle 빌드를 돌리는데, t3.micro(1GB)는 빌드 중 OOM 날 수 있다. 로컬/CI에서 이미지를 미리 빌드해 ECR로 배포하는 방식(6절)이면 t3.micro도 가능 |
| 스토리지 | 최소 20GB gp3 | Docker 베이스 이미지 + 빌드 캐시 + DB 볼륨 감안. 이 프로젝트 개발 중 실제로 디스크 부족으로 빌드가 깨진 적이 있다 |
| 키페어 | 새로 생성하거나 기존 것 사용 | SSH 접속용 |

## 2. 보안 그룹

| 포트 | 소스 | 용도 |
|---|---|---|
| 22 (SSH) | 내 IP만 | 관리 접속. `0.0.0.0/0`으로 열어두지 않는다 |
| 8080 | 앞단에 ALB/Nginx를 안 둘 경우 `0.0.0.0/0` | 애플리케이션 직접 노출 |
| 80/443 | ALB/Nginx를 둘 경우 | HTTPS 종단 |
| 5432 | **열지 않는다** | `docker-compose.yml`의 `db` 서비스는 `ports`를 노출하지 않아 컨테이너 내부 네트워크에서만 접근 가능하다. 보안그룹에서도 열 필요가 없다 |

## 3. Docker / Docker Compose 설치

```bash
# 인스턴스에 SSH 접속 후

# Amazon Linux 2023
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker $(whoami)

# Ubuntu 22.04
sudo apt update && sudo apt install -y docker.io docker-compose-plugin git
sudo systemctl enable --now docker
sudo usermod -aG docker $(whoami)
```

`usermod` 이후에는 **한 번 로그아웃 후 재접속**해야 `docker` 명령을 `sudo` 없이 쓸 수 있다.

Docker Compose가 별도 필요한 경우(Amazon Linux 2023은 `docker compose` 플러그인이 기본 포함 안 될 수 있음):

```bash
sudo dnf install -y docker-compose-plugin
```

## 4. 코드 배포

```bash
git clone https://github.com/<org>/docsuri-server.git
cd docsuri-server

cp .env.example .env
vi .env   # GEMINI_API_KEY 등 실제 값 채우기 — 이 파일은 git에 절대 커밋하지 않는다 (.gitignore에 등록되어 있음)

docker compose up -d --build
```

빌드가 끝나면 `docker compose ps`로 `app`, `db` 둘 다 `healthy`인지 확인한다.

## 5. 동작 확인

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP", ...}
```

인스턴스 밖에서: `http://<EC2 퍼블릭 IP>:8080/actuator/health` (보안그룹에 8080이 열려 있어야 함)

## 6. (선택) 이미지를 미리 빌드해서 배포하기

EC2에서 직접 Gradle 빌드를 돌리고 싶지 않다면(인스턴스 사양을 낮게 유지하고 싶을 때), 로컬이나 CI에서 이미지를 빌드해 ECR에 올리고 EC2는 pull만 하게 한다.

```bash
# 로컬 또는 CI에서
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
docker build -t <account-id>.dkr.ecr.<region>.amazonaws.com/docsuri-server:latest .
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/docsuri-server:latest
```

`docker-compose.yml`의 `app.build: .`를 아래처럼 바꾼다 (EC2 쪽 파일만):

```yaml
app:
  image: <account-id>.dkr.ecr.<region>.amazonaws.com/docsuri-server:latest
  # build: . 줄은 제거
```

EC2에서는:

```bash
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
docker compose pull
docker compose up -d
```

## 7. HTTPS / 도메인 연결

이 프로젝트는 TLS를 직접 처리하지 않는다. 둘 중 하나를 권장한다.

**옵션 A — ALB + ACM (권장, 관리 부담 적음)**
1. ACM에서 도메인 인증서 발급
2. Application Load Balancer 생성, 대상 그룹을 EC2:8080으로 연결, 헬스체크 경로 `/actuator/health`
3. 리스너 443(HTTPS, ACM 인증서 연결) → 대상 그룹, 80은 443으로 리다이렉트
4. Route 53에서 도메인을 ALB로 연결
5. EC2 보안그룹의 8080 인바운드 소스를 ALB 보안그룹으로 제한 (더 이상 `0.0.0.0/0`일 필요 없음)

**옵션 B — EC2에 Nginx + Certbot 직접 구성**
1. `sudo dnf install -y nginx` (또는 apt)
2. Nginx가 443/80을 받아 `localhost:8080`으로 리버스 프록시
3. `certbot --nginx`로 Let's Encrypt 인증서 발급/자동 갱신
4. 보안그룹은 80/443만 열고 8080은 닫음(로컬호스트 통신이므로 외부 노출 불필요)

## 8. 재배포 / 운영

```bash
# 코드 업데이트 후 재배포
git pull
docker compose up -d --build

# 로그 확인
docker compose logs -f app

# 재시작 (데이터 유지)
docker compose restart

# 완전히 내리기 (볼륨은 유지 — DB 데이터 안 지워짐)
docker compose down

# 볼륨까지 전부 삭제 (DB 데이터 소실 — 신중히)
docker compose down -v
```

- 인스턴스 재부팅 시: `db`/`app`은 `restart: unless-stopped`로 설정돼 있고, Docker 데몬 자체가 `systemctl enable docker`로 부팅 시 자동 시작하므로 별도 조치 없이 컨테이너가 다시 뜬다.
- **DB 백업**: `docker compose exec db pg_dump -U postgres docsuri > backup.sql`을 크론으로 주기 실행하거나, EBS 스냅샷을 정기적으로 뜬다. `db-data` 볼륨이 곧 DB 전체 상태다.

## 9. 보안 체크리스트

- [ ] `.env`는 인스턴스에도 git으로 올라가지 않았는가 (`.gitignore` 확인, `git status`로 재확인)
- [ ] 가능하면 `.env`를 직접 두지 말고 AWS Secrets Manager/Parameter Store에서 배포 스크립트가 값을 가져와 `.env`를 생성하도록 구성
- [ ] 보안그룹에서 5432(DB), 22(SSH, 내 IP 외)가 외부에 열려 있지 않은가
- [ ] `GEMINI_API_KEY`가 채팅/이슈/커밋 메시지 등에 평문으로 남아있지 않은가 (만약 남았다면 Google AI Studio에서 즉시 재발급)

## 10. 알아두면 좋은 제약사항

- **단일 인스턴스 전제**: 업로드/export 파일이 `app-storage` 볼륨(로컬 디스크)에 저장된다. 여러 인스턴스로 스케일아웃하면 파일 접근이 깨진다. 향후 S3로 옮기려면 `FileStorage` 인터페이스 구현체만 교체하면 된다 (`DECISIONS.md` 참고).
- **Gemini 무료 티어 쿼터**: `GEMINI_MODEL_REASONING`에 pro 계열 모델을 쓰면 무료 티어에서 쿼터가 0으로 막힐 수 있다 (개발 중 실제로 겪음). 배포 전 Google AI Studio에서 결제 활성화 여부를 확인할 것.
- **디스크 공간**: Docker 이미지 빌드/캐시가 생각보다 용량을 많이 먹는다. `docker system df`로 주기적으로 확인하고, 필요하면 `docker system prune`으로 정리한다.
