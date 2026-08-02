export interface FileItem {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  folderId?: number;
  uploadedAt: string;
}

export interface Folder {
  id: number;
  name: string;
  parentFolderId?: number;
  createdAt: string;
}
