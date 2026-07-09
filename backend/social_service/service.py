from __future__ import annotations

from shared.events.bus import event_bus
from shared.models import FAITH_REACTIONS, FaithReaction, NoteReactions, ReactionCount
from social_service.repositories.social import SocialRepository
from feed_service.cache import FeedCacheService


class SocialService:
    def __init__(self, social: SocialRepository, feed_cache: FeedCacheService) -> None:
        self._social = social
        self._feed_cache = feed_cache

    def toggle_reaction(
        self,
        note_id: str,
        user_id: str,
        reaction: FaithReaction,
    ) -> FaithReaction | None:
        active = self._social.toggle_reaction(note_id, user_id, reaction)
        self._feed_cache.invalidate_all_feeds()
        event_bus.publish(
            "ReactionToggled",
            {"note_id": note_id, "user_id": user_id, "reaction": reaction},
        )
        return active

    def get_reactions_batch(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> dict[str, list[tuple[FaithReaction, int, bool]]]:
        return self._social.get_reactions_batch(note_ids, viewer_id)

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        return self._social.get_comment_counts_batch(note_ids)

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        return self._social.total_engagement(note_id)

    def reactions_batch_response(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> list[NoteReactions]:
        batch = self.get_reactions_batch(note_ids, viewer_id)
        return [
            NoteReactions(
                note_id=note_id,
                reactions=[
                    ReactionCount(type=reaction, count=count, reacted_by_me=reacted_by_me)
                    for reaction, count, reacted_by_me in batch.get(note_id, [])
                ],
            )
            for note_id in note_ids
        ]
