import { apiClient } from "@/lib/apiClient";

// All folder endpoints live here. Components/hooks call these, never fetch directly.

// GET /folders/root/contents | /folders/{folderId}/contents -> FolderContentsResponse
// { currentFolder, breadcrumb: [{ id, name }] (root -> current, inclusive), subfolders, documents: Page }
// folderId = null targets the root folder; the response still carries root's real id.
export function getFolderContents(folderId, params, signal) {
  const path = folderId == null ? "/folders/root/contents" : `/folders/${folderId}/contents`;
  return apiClient.get(path, { params, signal });
}

// POST /folders -> FolderResponse. parentId is required by the backend (only root has no parent).
export function createFolder({ name, parentId }) {
  return apiClient.postJson("/folders", { name, parentId });
}

// PUT /folders/{folderId} -> FolderResponse
export function renameFolder(folderId, name) {
  return apiClient.putJson(`/folders/${folderId}`, { name });
}

// DELETE /folders/{folderId} -> soft delete (folder + its whole subtree go to trash)
export function deleteFolder(folderId) {
  return apiClient.del(`/folders/${folderId}`);
}

// GET /folders/deleted?keyword= -> Page<FolderResponse>, most recently deleted first.
export function getDeletedFolders(params, signal) {
  return apiClient.get("/folders/deleted", { params, signal });
}

// POST /folders/{folderId}/restore -> brings the folder and its whole subtree
// (subfolders + the documents deleted with it) back to ACTIVE.
export function restoreFolder(folderId) {
  return apiClient.postJson(`/folders/${folderId}/restore`);
}

// GET /folders/search?keyword=&page=&size= -> Page<FolderResponse> (ACTIVE only)
export function searchFolders(keyword, params, signal) {
  return apiClient.get("/folders/search", { params: { keyword, ...params }, signal });
}

// PUT /folders/{folderId}/move -> FolderResponse. targetParentId is required — pass
// the root folder's real id (from getFolderContents' currentFolder.id) to move to root.
export function moveFolder(folderId, targetParentId) {
  return apiClient.putJson(`/folders/${folderId}/move`, { targetParentId });
}
