FROM python:3.12-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc libffi-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt pyproject.toml ./
RUN pip install --no-cache-dir -r requirements.txt

COPY common ./common
COPY schema ./schema
COPY notification_service ./notification_service

ENV PYTHONPATH=/app
EXPOSE 8007

CMD ["uvicorn", "notification_service.main:app", "--host", "0.0.0.0", "--port", "8007"]
