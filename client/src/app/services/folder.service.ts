import { Injectable } from "@angular/core";
import { HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { Folder } from "../models/file.model";
import { ApiService } from "./api.service";

@Injectable({
  providedIn: "root",
})
export class FolderService {
  constructor(private apiService: ApiService) {}

  createFolder(name: string, parentFolderId?: number): Observable<Folder> {
    return this.apiService.post<Folder>("/folders", { name, parentFolderId });
  }

  getFolders(parentFolderId?: number): Observable<Folder[]> {
    let params = new HttpParams();
    if (parentFolderId) {
      params = params.set("parentFolderId", parentFolderId.toString());
    }
    return this.apiService.get<Folder[]>("/folders", { params });
  }

  updateFolder(folderId: number, name: string): Observable<Folder> {
    return this.apiService.put<Folder>(`/folders/${folderId}`, { name });
  }

  deleteFolder(folderId: number): Observable<any> {
    return this.apiService.delete(`/folders/${folderId}`);
  }

  downloadFolder(folderId: number): Observable<Blob> {
    return this.apiService.downloadFile(`/folders/download/${folderId}`);
  }
}
