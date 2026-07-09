FROM python:3.12-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc libffi-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt pyproject.toml ./
RUN pip install --no-cache-dir -r requirements.txt

COPY shared ./shared
COPY user_service ./user_service
COPY note_service ./note_service
COPY social_service ./social_service
COPY feed_service ./feed_service

ENV PYTHONPATH=/app
EXPOSE 8004

CMD ["uvicorn", "feed_service.main:app", "--host", "0.0.0.0", "--port", "8004"]
