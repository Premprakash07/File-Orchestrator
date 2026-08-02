import { Injectable } from "@angular/core";
import { HttpParams, HttpClient } from "@angular/common/http";
import { Observable, switchMap, from } from "rxjs";
import { FileItem } from "../models/file.model";
import { ApiService } from "./api.service";

interface PresignedUrlResponse {
  uploadUrl: string;
  s3Key: string;
  expirationMinutes: number;
}

interface UploadConfirmation {
  s3Key: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  folderId?: number;
}

@Injectable({
  providedIn: "root",
})
export class FileService {
  constructor(
    private apiService: ApiService,
    private http: HttpClient,
  ) {}

  /**
   * New S3 Upload Method - Request presigned URL, upload to S3, then confirm
   */
  uploadFileToS3(
    file: File,
    folderId?: number,
    aiSummarize?: boolean,
    extractMetadata?: boolean,
    generateThumbnail?: boolean,
    tags?: string,
  ): Observable<FileItem> {
    // Step 1: Get presigned URL from backend
    let params = new HttpParams()
      .set("fileName", file.name)
      .set("contentType", file.type || "application/octet-stream");

    return this.apiService
      .post<PresignedUrlResponse>("/files/presigned-url", null, { params })
      .pipe(
        switchMap((presignedResponse) => {
          // Step 2: Upload file directly to S3
          return from(
            fetch(presignedResponse.uploadUrl, {
              method: "PUT",
              body: file,
              headers: {
                "Content-Type": file.type || "application/octet-stream",
              },
            }).then((response) => {
              if (!response.ok) {
                throw new Error("S3 upload failed");
              }
              return presignedResponse;
            }),
          );
        }),
        switchMap((presignedResponse) => {
          // Step 3: Confirm upload with backend
          const confirmationData: UploadConfirmation = {
            s3Key: presignedResponse.s3Key,
            fileName: file.name,
            fileType: file.type || "application/octet-stream",
            fileSize: file.size,
            folderId: folderId,
          };
          return this.apiService.post<FileItem>(
            "/files/confirm-upload",
            confirmationData,
          );
        }),
      );
  }

  /**
   * Legacy Direct Upload Method (kept for backward compatibility)
   */
  uploadFile(
    file: File,
    folderId?: number,
    aiSummarize?: boolean,
    extractMetadata?: boolean,
    generateThumbnail?: boolean,
    tags?: string,
  ): Observable<FileItem> {
    const formData = new FormData();
    formData.append("file", file);
    if (folderId) {
      formData.append("folderId", folderId.toString());
    }
    if (aiSummarize !== undefined) {
      formData.append("aiSummarize", aiSummarize.toString());
    }
    if (extractMetadata !== undefined) {
      formData.append("extractMetadata", extractMetadata.toString());
    }
    if (generateThumbnail !== undefined) {
      formData.append("generateThumbnail", generateThumbnail.toString());
    }
    if (tags) {
      formData.append("tags", tags);
    }
    return this.apiService.uploadFile<FileItem>("/files/upload", formData);
  }

  getFiles(folderId?: number): Observable<FileItem[]> {
    let params = new HttpParams();
    if (folderId) {
      params = params.set("folderId", folderId.toString());
    }
    return this.apiService.get<FileItem[]>("/files", { params });
  }

  downloadFile(fileId: number): Observable<Blob> {
    return this.apiService.downloadFile(`/files/download/${fileId}`);
  }

  deleteFile(fileId: number): Observable<any> {
    return this.apiService.delete(`/files/${fileId}`);
  }
}
