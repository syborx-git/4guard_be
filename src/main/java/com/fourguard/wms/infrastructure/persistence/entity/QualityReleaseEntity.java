package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.ReleaseAuthorizerType;
import com.fourguard.wms.domain.enums.ReleaseDestination;
import com.fourguard.wms.domain.enums.ReleaseSupportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
    name = "quality_releases",
    schema = "wms",
    indexes = {
        @Index(name = "idx_qm_releases_org", columnList = "organization_id, branch_id"),
        @Index(name = "idx_qm_releases_item", columnList = "item_id"),
        @Index(name = "idx_qm_releases_incidence", columnList = "incidence_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class QualityReleaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false, unique = true, length = 30)
    private String folio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidence_id", nullable = false)
    private IncidenceEntity incidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItemEntity item;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorizer_type", nullable = false, length = 30)
    private ReleaseAuthorizerType authorizerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_type", nullable = false, length = 30)
    private ReleaseSupportType supportType;

    @Column(name = "support_custom_type", length = 100)
    private String supportCustomType;

    @Column(name = "support_subject", nullable = false, length = 255)
    private String supportSubject;

    @Column(name = "support_file_name", length = 255)
    private String supportFileName;

    @Column(name = "authorized_by_name", nullable = false, length = 150)
    private String authorizedByName;

    @Column(name = "authorized_by_position", nullable = false, length = 150)
    private String authorizedByPosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReleaseDestination destination;

    @Column(name = "decision_notes", nullable = false, columnDefinition = "TEXT")
    private String decisionNotes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "released_by_user_id", nullable = false)
    private UserEntity releasedByUser;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_metadata", columnDefinition = "JSONB")
    private String evidenceMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
