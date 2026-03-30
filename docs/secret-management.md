# Secret management

## Muc tieu

- Khong commit `src/main/resources/application.yml` vao Git.
- Luu cau hinh that o may local hoac GitHub Secrets.
- Van giu duoc auto deploy khi push len `main`.

## Local development

1. Copy `src/main/resources/application.example.yml` thanh `src/main/resources/application.yml`.
2. Dien gia tri that vao file local vua tao.
3. Khong commit file do len repo.

## GitHub deploy

Workflow `auto-build-deploy-java.yaml` se tao lai `src/main/resources/application.yml`
tu GitHub secret `BACKEND_APPLICATION_YML` truoc khi build Docker image.

Ban can tao GitHub secret:

- `BACKEND_APPLICATION_YML`: toan bo noi dung that cua file `application.yml`
- `IMAGE_NAME`
- `DOCKER_HOST_PORT`
- `APP_INTERNAL_PORT`

## Gemini key rotation

Neu Gemini API key da bi lo, lam theo thu tu nay:

1. Tao Gemini API key moi.
2. Cap nhat key moi trong file local `application.yml`.
3. Cap nhat key moi trong GitHub secret `BACKEND_APPLICATION_YML`.
4. Push len `main` de workflow deploy lai.
5. Kiem tra chatbot hoat dong voi key moi.
6. Thu hoi key cu neu chua thu hoi.

Neu ban revoke key cu truoc khi production duoc deploy lai voi key moi,
tinh nang Gemini Chatbot se tam thoi loi cho den khi container moi duoc deploy.
