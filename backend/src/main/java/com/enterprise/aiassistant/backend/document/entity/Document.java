package com.enterprise.aiassistant.backend.document.entity;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(
                        name = "idx_document_current_version_id",
                        columnList = "current_version_id"),
                @Index(
                        name = "idx_document_status",
                        columnList = "status"),
                @Index(
                        name = "idx_document_type",
                        columnList = "document_type"),
                @Index(
                        name = "idx_document_created_at",
                        columnList = "created_at"),
                @Index(
                        name = "idx_document_folder_id",
                        columnList = "folder_id"),
                @Index(
                        name = "idx_document_owner_id",
                        columnList = "owner_id"),
                @Index(
                        name = "idx_document_department_id",
                        columnList = "department_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DocumentVersion currentVersion;

    // Luôn có giá trị: không chỉ định folder khi upload thì mặc định là thư mục gốc
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "document_type")
    private DocumentType documentType;

    // Nullable vì dữ liệu cũ được tạo trước khi có ownership. Document không có owner/department
    // được coi là tài liệu dùng chung: mọi người có DOCUMENT_READ đều đọc được, chỉ ADMIN sửa được.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<DocumentVersion> versions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
