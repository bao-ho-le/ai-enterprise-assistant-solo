package com.enterprise.aiassistant.backend.folder.entity;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "folders",
        indexes = {
                @Index(
                        name = "idx_folder_parent_id",
                        columnList = "parent_id"),
                @Index(
                        name = "idx_folder_status",
                        columnList = "status"),
                @Index(
                        name = "idx_folder_created_at",
                        columnList = "created_at"),
                @Index(
                        name = "idx_folder_owner_id",
                        columnList = "owner_id"),
                @Index(
                        name = "idx_folder_department_id",
                        columnList = "department_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    // null CHỈ dành cho root — mọi folder khác bắt buộc có cha (validate ở FolderService)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    // Null = folder dùng chung (thư mục gốc và dữ liệu cũ): mọi role có FOLDER_READ đều đọc được.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private FolderStatus status = FolderStatus.ACTIVE;

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
            mappedBy = "parent",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Folder> children = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
