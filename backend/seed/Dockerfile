FROM alpine:3.20

RUN apk add --no-cache bash curl jq

COPY seed-dev-data.sh /seed/seed-dev-data.sh
RUN chmod +x /seed/seed-dev-data.sh

ENTRYPOINT ["/seed/seed-dev-data.sh"]
