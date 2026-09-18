FROM python:3.12-slim
WORKDIR /app

# Pulls in Debian's own security-patched package versions at build time,
# since the base image tag's baked-in packages lag behind by the time we
# build (caught by the Phase 15 Trivy image scan).
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Dependency layer cached separately from app code. requirements.txt keeps
# runtime and dev/test/lint tooling together for a project this size (see
# its own header comment) — installed as-is rather than splitting it here.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

RUN useradd --system --create-home appuser
USER appuser

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/internal/v1/health')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
