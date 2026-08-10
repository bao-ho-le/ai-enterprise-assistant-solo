package com.enterprise.aiassistant.backend.folder.mapper;

import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.BreadcrumbItem;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class FolderMapper {

    public Folder toFolder(CreateFolderRequest request, Folder parent) {
        return Folder.builder()
                .name(request.getName())
                .parent(parent)
                .build();
    }

    public FolderResponse toFolderResponse(Folder folder) {
        if (folder == null) {
            return null;
        }
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .status(folder.getStatus())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .deletedAt(folder.getDeletedAt())
                .build();
    }

    public List<FolderResponse> toFolderResponseList(List<Folder> folders) {
        return folders.stream()
                .map(this::toFolderResponse)
                .toList();
    }

    // Đi ngược từ folder hiện tại lên root để build breadcrumb: [Root's child, ..., Parent, Current]
    public List<BreadcrumbItem> toBreadcrumb(Folder folder) {
        List<BreadcrumbItem> breadcrumb = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            breadcrumb.add(new BreadcrumbItem(current.getId(), current.getName()));
            current = current.getParent();
        }
        Collections.reverse(breadcrumb);
        return breadcrumb;
    }
}
