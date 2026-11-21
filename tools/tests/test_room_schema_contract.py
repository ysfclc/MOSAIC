import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PERSISTENCE = ROOT / "core/persistence/src/main/java/com/ysfclc/mosaic/core/persistence"


class RoomV1ContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.database = (PERSISTENCE / "MosaicDatabase.kt").read_text(encoding="utf-8")
        cls.entities = (PERSISTENCE / "LibraryEntities.kt").read_text(encoding="utf-8")
        cls.build = (ROOT / "core/persistence/build.gradle.kts").read_text(encoding="utf-8")

    def test_database_version_and_schema_export_are_explicit(self):
        self.assertIn("version = 1", self.database)
        self.assertIn("exportSchema = true", self.database)
        self.assertIn('arg("room.schemaLocation", "$projectDir/schemas")', self.build)
        self.assertNotIn("fallbackToDestructiveMigration", self.database)

    def test_v1_entity_set_is_registered(self):
        expected = {
            "MediaItemEntity", "MediaLocationEntity", "MediaCollectionEntity",
            "MediaCollectionItemEntity", "PlaybackHistoryEntity",
            "PlaybackQueueStateEntity", "PlaybackQueueEntryEntity",
            "PlaybackTimerStateEntity", "MigrationMarkerEntity",
        }
        registered = self.database.split("entities = [", 1)[1].split("]", 1)[0]
        for entity in expected:
            with self.subTest(entity=entity):
                self.assertIn(entity + "::class", registered)

    def test_media_identity_is_unique_per_source(self):
        entity = self.entities.split('tableName = "media_items"', 1)[1].split("data class MediaItemEntity", 1)[0]
        self.assertIn('Index(value = ["sourceId", "sourceItemId"], unique = true)', entity)

    def test_location_and_collection_relationships_cascade_with_parent(self):
        location = self.entities.split('tableName = "media_locations"', 1)[1].split("data class MediaLocationEntity", 1)[0]
        membership = self.entities.split('tableName = "media_collection_items"', 1)[1].split("data class MediaCollectionItemEntity", 1)[0]
        self.assertIn("onDelete = ForeignKey.CASCADE", location)
        self.assertEqual(membership.count("onDelete = ForeignKey.CASCADE"), 2)

    def test_history_and_queue_snapshots_survive_media_item_deletion(self):
        history = self.entities.split('tableName = "playback_history"', 1)[1].split("data class PlaybackHistoryEntity", 1)[0]
        queue = self.entities.split('tableName = "playback_queue_entries"', 1)[1].split("data class PlaybackQueueEntryEntity", 1)[0]
        self.assertIn("onDelete = ForeignKey.SET_NULL", history)
        self.assertIn("onDelete = ForeignKey.SET_NULL", queue)

    def test_queue_state_and_timer_are_first_class_persistent_entities(self):
        self.assertIn("PlaybackQueueStateEntity::class", self.database)
        self.assertIn("PlaybackQueueEntryEntity::class", self.database)
        self.assertIn("PlaybackTimerStateEntity::class", self.database)
        self.assertIn("abstract fun playbackQueue(): PlaybackQueueDao", self.database)
        self.assertIn("abstract fun playbackTimer(): PlaybackTimerDao", self.database)


if __name__ == "__main__":
    unittest.main()
