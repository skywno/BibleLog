#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://traefik:8000}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-120}"

log() {
  printf '[dev-seed] %s\n' "$*"
}

wait_for_api() {
  local elapsed=0
  log "Waiting for API at ${API_BASE}/health ..."
  until curl -sf "${API_BASE}/health" >/dev/null 2>&1; do
    if (( elapsed >= MAX_WAIT_SECONDS )); then
      log "ERROR: API not ready after ${MAX_WAIT_SECONDS}s"
      exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  log "API is ready."
}

dev_login() {
  local email="$1"
  curl -sf -X POST "${API_BASE}/auth/dev/login?email=${email}"
}

api_get() {
  local token="$1"
  local path="$2"
  curl -sf -H "Authorization: Bearer ${token}" "${API_BASE}${path}"
}

api_post() {
  local token="$1"
  local path="$2"
  local body="${3:-}"
  if [[ -n "${body}" ]]; then
    curl -sf -X POST \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      -d "${body}" \
      "${API_BASE}${path}"
  else
    curl -sf -X POST \
      -H "Authorization: Bearer ${token}" \
      "${API_BASE}${path}"
  fi
}

api_patch() {
  local token="$1"
  local path="$2"
  local body="$3"
  curl -sf -X PATCH \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${body}" \
    "${API_BASE}${path}"
}

is_seeded() {
  local token="$1"
  local count
  count="$(api_get "${token}" "/churches/search?q=Grace&limit=1" | jq 'length')"
  [[ "${count}" -gt 0 ]]
}

join_church() {
  local token="$1"
  local church_id="$2"
  api_post "${token}" "/churches/${church_id}/members" || true
}

join_group() {
  local token="$1"
  local group_id="$2"
  api_post "${token}" "/small-groups/${group_id}/members" || true
}

create_friendship() {
  local from_token="$1"
  local to_user_id="$2"
  local accept_token="$3"

  if api_get "${from_token}" "/friends" | jq -e --arg id "${to_user_id}" '.[] | select(.id == $id)' >/dev/null; then
    log "Already friends (${to_user_id}), skipping."
    return 0
  fi

  api_post "${from_token}" "/friends/requests" "{\"to_user_id\":\"${to_user_id}\"}" >/dev/null

  local request_id
  request_id="$(api_get "${accept_token}" "/friends/requests/incoming" \
    | jq -r '.[0].id // empty')"
  if [[ -z "${request_id}" ]]; then
    log "WARNING: no incoming friend request found for ${to_user_id}"
    return 0
  fi

  api_post "${accept_token}" "/friends/requests/${request_id}/accept" >/dev/null
}

create_note() {
  local token="$1"
  local body="$2"
  api_post "${token}" "/journal/notes" "${body}" >/dev/null
}

wait_for_api

log "Logging in seed users ..."
GRACE_RESP="$(dev_login "grace@biblelog.dev")"
DAVID_RESP="$(dev_login "david@biblelog.dev")"
SARAH_RESP="$(dev_login "sarah@biblelog.dev")"
MARK_RESP="$(dev_login "mark@biblelog.dev")"
DEMO_RESP="$(dev_login "demo@biblelog.app")"

GRACE_TOKEN="$(jq -r '.access_token' <<<"${GRACE_RESP}")"
DAVID_TOKEN="$(jq -r '.access_token' <<<"${DAVID_RESP}")"
SARAH_TOKEN="$(jq -r '.access_token' <<<"${SARAH_RESP}")"
MARK_TOKEN="$(jq -r '.access_token' <<<"${MARK_RESP}")"
DEMO_TOKEN="$(jq -r '.access_token' <<<"${DEMO_RESP}")"

DAVID_ID="$(jq -r '.user.id' <<<"${DAVID_RESP}")"
SARAH_ID="$(jq -r '.user.id' <<<"${SARAH_RESP}")"
MARK_ID="$(jq -r '.user.id' <<<"${MARK_RESP}")"

if is_seeded "${GRACE_TOKEN}"; then
  log "Seed data already present, skipping."
  exit 0
fi

log "Creating churches ..."
GRACE_CHURCH="$(api_post "${GRACE_TOKEN}" "/churches" \
  '{"name":"Grace Community Church","description":"테스트용 공동체 교회"}')"
GRACE_CHURCH_ID="$(jq -r '.id' <<<"${GRACE_CHURCH}")"

join_church "${DAVID_TOKEN}" "${GRACE_CHURCH_ID}"
join_church "${SARAH_TOKEN}" "${GRACE_CHURCH_ID}"

HOPE_CHURCH="$(api_post "${MARK_TOKEN}" "/churches" \
  '{"name":"Hope Chapel","description":"테스트용 소형 교회"}')"
HOPE_CHURCH_ID="$(jq -r '.id' <<<"${HOPE_CHURCH}")"

log "Creating small groups ..."
MONDAY_GROUP="$(api_post "${DAVID_TOKEN}" "/small-groups" \
  "{\"name\":\"Monday Prayer Group\",\"church_id\":\"${GRACE_CHURCH_ID}\"}")"
MONDAY_GROUP_ID="$(jq -r '.id' <<<"${MONDAY_GROUP}")"

YOUTH_GROUP="$(api_post "${DAVID_TOKEN}" "/small-groups" \
  "{\"name\":\"Youth Fellowship\",\"church_id\":\"${GRACE_CHURCH_ID}\"}")"
YOUTH_GROUP_ID="$(jq -r '.id' <<<"${YOUTH_GROUP}")"

HOPE_GROUP="$(api_post "${MARK_TOKEN}" "/small-groups" \
  "{\"name\":\"Hope Bible Study\",\"church_id\":\"${HOPE_CHURCH_ID}\"}")"
HOPE_GROUP_ID="$(jq -r '.id' <<<"${HOPE_GROUP}")"

join_group "${SARAH_TOKEN}" "${YOUTH_GROUP_ID}"
join_group "${MARK_TOKEN}" "${HOPE_GROUP_ID}"

log "Creating friendships ..."
create_friendship "${GRACE_TOKEN}" "${DAVID_ID}" "${DAVID_TOKEN}"
create_friendship "${SARAH_TOKEN}" "${MARK_ID}" "${MARK_TOKEN}"

log "Configuring private profile (Sarah) ..."
api_patch "${SARAH_TOKEN}" "/users/me" \
  '{"photo_url":"https://picsum.photos/seed/sarah","profile_visibility":"private"}' >/dev/null

log "Creating pending follow request (Grace -> Sarah) ..."
api_post "${GRACE_TOKEN}" "/follows/${SARAH_ID}" >/dev/null || true

log "Creating journal notes ..."
create_note "${GRACE_TOKEN}" \
  '{"content":"오늘 말씀을 읽으며 하나님의 사랑을 다시금 깨달았습니다.","prayer_topic":"가정의 평안","emotion":"gratitude","visibility":"public"}'
create_note "${GRACE_TOKEN}" \
  '{"content":"교회 공동체와 함께 나눈 묵상입니다. 서로를 위해 기도합시다.","prayer_topic":"교회의 연합","emotion":"peace","visibility":"church"}'

create_note "${DAVID_TOKEN}" \
  '{"content":"시편 23편을 묵상하며 목자 되신 하나님께 감사드립니다.","prayer_topic":"인도하심","emotion":"joy","visibility":"public"}'
create_note "${DAVID_TOKEN}" \
  '{"content":"월요 기도 모임에서 나눈 묵상 기록입니다.","prayer_topic":"소그룹의 성장","emotion":"moved","visibility":"small_group"}'

create_note "${SARAH_TOKEN}" \
  '{"content":"친구와 함께 나눌 묵상입니다. 주님의 은혜가 충만하기를.","prayer_topic":"친구를 위한 기도","emotion":"gratitude","visibility":"friends"}'

create_note "${MARK_TOKEN}" \
  '{"content":"나만의 기도 일기입니다.","prayer_topic":"개인 회개","emotion":"sadness","visibility":"private"}'

create_note "${DEMO_TOKEN}" \
  '{"content":"데모 계정의 첫 묵상 노트입니다.","prayer_topic":"새로운 시작","emotion":"peace","visibility":"public"}'

log "Seed complete."
