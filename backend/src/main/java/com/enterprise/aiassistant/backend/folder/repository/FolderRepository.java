package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long>,
        FolderRepositoryCustom {

    // Root được định danh bằng "tên root + không có cha"; dùng lúc khởi tạo và mỗi khi cần giải ra root thật.
    Optional<Folder> findFirstByNameAndParentIsNull(String name);

    // Backfill lúc khởi động: mọi folder không có cha (trừ root) đều là dữ liệu cũ cần gắn lại vào root.
    List<Folder> findByParentIsNull();

    List<Folder> findByParentIdAndStatusOrderByNameAsc(Long parentId, FolderStatus status);

    // Dùng cho hard delete: lấy TẤT CẢ folder con trực tiếp bất kể status
    // (để đảm bảo xoá sạch toàn bộ cây, kể cả trường hợp dữ liệu không đồng bộ status).
    List<Folder> findByParentId(Long parentId);

    // Thùng rác: liệt kê các folder đã xoá mềm, mới xoá gần nhất lên trước.
    Page<Folder> findByStatusOrderByDeletedAtDesc(FolderStatus status, Pageable pageable);

    // Thùng rác có lọc theo tên (không phân biệt hoa/thường).
    Page<Folder> findByStatusAndNameContainingIgnoreCaseOrderByDeletedAtDesc(
            FolderStatus status, String keyword, Pageable pageable);

    // Tìm kiếm folder theo tên (không phân biệt hoa/thường), chỉ trong các folder đang ACTIVE.
    Page<Folder> findByNameContainingIgnoreCaseAndStatusOrderByNameAsc(
            String keyword, FolderStatus status, Pageable pageable);

    // parent == null sẽ tự động được Spring Data JPA dịch thành "parent IS NULL"
    boolean existsByNameAndParentAndStatus(String name, Folder parent, FolderStatus status);

    boolean existsByNameAndParentAndStatusAndIdNot(String name, Folder parent, FolderStatus status, Long id);
}
