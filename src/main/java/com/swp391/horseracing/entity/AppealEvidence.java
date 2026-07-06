package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.AppealEvidenceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "appeal_evidences")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evidence_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID evidenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appeal_id", nullable = false)
    Appeal appeal;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    AppealEvidenceType type;

    @Column(name = "file_url", columnDefinition = "TEXT")
    String fileUrl;

    @Column(name = "text_content", columnDefinition = "TEXT")
    String textContent;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    LocalDateTime uploadedAt;
}
