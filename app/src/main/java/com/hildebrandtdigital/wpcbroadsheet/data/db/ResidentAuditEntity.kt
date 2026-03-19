package com.hildebrandtdigital.wpcbroadsheet.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable audit log for resident lifecycle events.
 *
 * A new row is appended for every mutation — rows are NEVER updated or deleted.
 * This gives a complete, queryable history for any resident.
 *
 * [fromSiteId] / [toSiteId] are only populated for RELOCATED events, making
 * relocation history directly queryable without parsing free-text [note].
 *
 * Other actions leave both site fields null.
 *
 * Actions and their field usage:
 * ┌─────────────┬────────────┬──────────┬──────────┬────────────────────┐
 * │ action      │ siteId     │ fromSite │ toSite   │ note               │
 * ├─────────────┼────────────┼──────────┼──────────┼────────────────────┤
 * │ CREATED     │ new site   │ null     │ null     │ optional           │
 * │ UPDATED     │ their site │ null     │ null     │ what changed       │
 * │ DEACTIVATED │ their site │ null     │ null     │ reason (optional)  │
 * │ REACTIVATED │ their site │ null     │ null     │ optional           │
 * │ RELOCATED   │ FROM site  │ fromSite │ toSite   │ optional           │
 * └─────────────┴────────────┴──────────┴──────────┴────────────────────┘
 *
 * Note: for RELOCATED, [siteId] holds the origin site so the record stays
 * queryable under the resident's previous site — the new site's records
 * will naturally grow from the new [toSiteId] onwards.
 */
@Entity(
    tableName = "resident_audit",
    indices   = [
        Index("siteId", "unitNumber"),  // fast per-resident history lookup
        Index("action"),                // filter by event type
        Index("actor"),                 // filter by who acted
        Index("at"),                    // filter / sort by time
    ]
)
data class ResidentAuditEntity(

    @PrimaryKey(autoGenerate = true)
    val id         : Long    = 0,

    /**
     * The site the resident belonged to at the time of the event.
     * For RELOCATED events this is the FROM site (origin).
     */
    val siteId     : String,
    val unitNumber : String,

    /** [AuditAction].name — stored as String for schema forward-compatibility. */
    val action     : String,

    /** Username or email of the user who triggered the action. */
    val actor      : String,

    /** Epoch milliseconds. Convert via Instant.ofEpochMilli(at) for display. */
    val at         : Long,

    // ── Relocation fields — non-null only for RELOCATED events ─────────────
    /** Site the resident was moved FROM. Null for all non-relocation actions. */
    val fromSiteId : String? = null,

    /** Site the resident was moved TO. Null for all non-relocation actions. */
    val toSiteId   : String? = null,

    /** Optional free-text context, e.g. reason for deactivation. */
    val note       : String? = null,
)
