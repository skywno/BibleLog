FROM python:3.12-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc libffi-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt pyproject.toml ./
RUN pip install --no-cache-dir -r requirements.txt

COPY shared ./shared
COPY schema ./schema
COPY feed_service ./feed_service
COPY social_service ./social_service

ENV PYTHONPATH=/app
EXPOSE 8003

CMD ["uvicorn", "social_service.main:app", "--host", "0.0.0.0", "--port", "8003"]
