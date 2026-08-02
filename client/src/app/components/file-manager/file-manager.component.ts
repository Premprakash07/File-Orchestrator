import { Component, OnInit, ViewChild } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../services/auth.service";
import { FileService } from "../../services/file.service";
import { FolderService } from "../../services/folder.service";
import { ModalService } from "../../services/modal.service";
import { TranslatePipe } from "../../pipes/translate.pipe";
import { FileItem, Folder } from "../../models/file.model";
import {
  FileUploadModalComponent,
  FileUploadOptions,
} from "../modals/file-upload-modal/file-upload-modal.component";
import { HeaderComponent } from "../header/header.component";

@Component({
  selector: "app-file-manager",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    FileUploadModalComponent,
    HeaderComponent,
  ],
  templateUrl: "./file-manager.component.html",
  styleUrls: ["./file-manager.component.css"],
})
export class FileManagerComponent implements OnInit {
  currentUser: any;
  folders: Folder[] = [];
  files: FileItem[] = [];
  currentFolderId?: number;
  folderPath: Folder[] = [];
  showCreateFolderInput = false;
  newFolderName = "";
  message = "";
  messageType: "success" | "error" = "success";
  viewMode: "list" | "gallery" = "gallery";
  selectedItem: { type: "folder" | "file"; data: Folder | FileItem } | null =
    null;
  private translate = new TranslatePipe();
  @ViewChild(FileUploadModalComponent) uploadModal!: FileUploadModalComponent;

  constructor(
    private authService: AuthService,
    private fileService: FileService,
    private folderService: FolderService,
    private modalService: ModalService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe((user) => {
      this.currentUser = user;
    });
    this.loadContent();
  }

  loadContent(): void {
    this.folderService.getFolders(this.currentFolderId).subscribe((folders) => {
      this.folders = folders;
    });

    this.fileService.getFiles(this.currentFolderId).subscribe((files) => {
      this.files = files;
    });
  }

  selectItem(type: "folder" | "file", data: Folder | FileItem): void {
    this.selectedItem = { type, data };
  }

  isSelected(type: "folder" | "file", id: number): boolean {
    return (
      this.selectedItem !== null &&
      this.selectedItem.type === type &&
      this.selectedItem.data.id === id
    );
  }

  navigateToFolder(folder: Folder): void {
    this.currentFolderId = folder.id;

    // Update breadcrumb path
    const folderIndex = this.folderPath.findIndex((f) => f.id === folder.id);
    if (folderIndex >= 0) {
      this.folderPath = this.folderPath.slice(0, folderIndex + 1);
    } else {
      this.folderPath.push(folder);
    }

    this.selectedItem = null;
    this.loadContent();
  }

  navigateToRoot(): void {
    this.currentFolderId = undefined;
    this.folderPath = [];
    this.selectedItem = null;
    this.loadContent();
  }

  createFolder(): void {
    if (this.newFolderName.trim()) {
      this.folderService
        .createFolder(this.newFolderName, this.currentFolderId)
        .subscribe({
          next: () => {
            this.showMessage(
              this.translate.transform("fileManager.messages.folderCreated"),
              "success",
            );
            this.newFolderName = "";
            this.showCreateFolderInput = false;
            this.loadContent();
          },
          error: () =>
            this.showMessage(
              this.translate.transform(
                "fileManager.messages.folderCreateFailed",
              ),
              "error",
            ),
        });
    }
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.uploadModal.open(file);
      // Reset input so same file can be selected again
      event.target.value = "";
    }
  }

  handleFileUpload(options: FileUploadOptions): void {
    this.fileService
      .uploadFileToS3(
        options.file,
        this.currentFolderId,
        options.aiSummarize,
        options.extractMetadata,
        options.generateThumbnail,
        options.tags,
      )
      .subscribe({
        next: () => {
          this.showMessage(
            this.translate.transform("fileManager.messages.fileUploaded"),
            "success",
          );
          this.loadContent();
        },
        error: () =>
          this.showMessage(
            this.translate.transform("fileManager.messages.fileUploadFailed"),
            "error",
          ),
      });
  }

  downloadFile(fileId: number, fileName: string): void {
    const message = `${this.translate.transform("fileManager.modals.downloadFile.message")}\n\n"${fileName}"`;

    this.modalService
      .confirm(
        this.translate.transform("fileManager.modals.downloadFile.title"),
        message,
        this.translate.transform("common.ok"),
        this.translate.transform("common.cancel"),
        "primary",
      )
      .then((confirmed) => {
        if (!confirmed) return;

        this.fileService.downloadFile(fileId).subscribe({
          next: (blob) => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = fileName;
            a.click();
            window.URL.revokeObjectURL(url);
          },
          error: () =>
            this.showMessage(
              this.translate.transform(
                "fileManager.messages.fileDownloadFailed",
              ),
              "error",
            ),
        });
      });
  }

  downloadFolder(folderId: number, folderName: string): void {
    const message = `${this.translate.transform("fileManager.modals.downloadFolder.message")}\n\n"${folderName}"`;

    this.modalService
      .confirm(
        this.translate.transform("fileManager.modals.downloadFolder.title"),
        message,
        this.translate.transform("common.ok"),
        this.translate.transform("common.cancel"),
        "primary",
      )
      .then((confirmed) => {
        if (!confirmed) return;

        this.folderService.downloadFolder(folderId).subscribe({
          next: (blob) => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `${folderName}.zip`;
            a.click();
            window.URL.revokeObjectURL(url);
          },
          error: () =>
            this.showMessage(
              this.translate.transform(
                "fileManager.messages.folderDownloadFailed",
              ),
              "error",
            ),
        });
      });
  }

  deleteFile(fileId: number): void {
    this.modalService
      .confirm(
        this.translate.transform("fileManager.modals.deleteFile.title"),
        this.translate.transform("fileManager.modals.deleteFile.message"),
        this.translate.transform("common.delete"),
        this.translate.transform("common.cancel"),
        "danger",
      )
      .then((confirmed) => {
        if (confirmed) {
          this.fileService.deleteFile(fileId).subscribe({
            next: () => {
              this.showMessage(
                this.translate.transform("fileManager.messages.fileDeleted"),
                "success",
              );
              this.selectedItem = null;
              this.loadContent();
            },
            error: () =>
              this.showMessage(
                this.translate.transform(
                  "fileManager.messages.fileDeleteFailed",
                ),
                "error",
              ),
          });
        }
      });
  }

  deleteFolder(folderId: number): void {
    this.modalService
      .confirm(
        this.translate.transform("fileManager.modals.deleteFolder.title"),
        this.translate.transform("fileManager.modals.deleteFolder.message"),
        this.translate.transform("common.delete"),
        this.translate.transform("common.cancel"),
        "danger",
      )
      .then((confirmed) => {
        if (confirmed) {
          this.folderService.deleteFolder(folderId).subscribe({
            next: () => {
              this.showMessage(
                this.translate.transform("fileManager.messages.folderDeleted"),
                "success",
              );
              this.selectedItem = null;
              this.loadContent();
            },
            error: () =>
              this.showMessage(
                this.translate.transform(
                  "fileManager.messages.folderDeleteFailed",
                ),
                "error",
              ),
          });
        }
      });
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0)
      return this.translate.transform("fileManager.fileSize.zero");
    const k = 1024;
    const sizes = [
      this.translate.transform("fileManager.fileSize.bytes"),
      this.translate.transform("fileManager.fileSize.kb"),
      this.translate.transform("fileManager.fileSize.mb"),
      this.translate.transform("fileManager.fileSize.gb"),
    ];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  }

  formatDate(date: string | Date): string {
    const d = new Date(date);
    return d.toLocaleDateString() + " " + d.toLocaleTimeString();
  }

  showMessage(msg: string, type: "success" | "error"): void {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => (this.message = ""), 3000);
  }

  toggleViewMode(mode: "list" | "gallery"): void {
    this.viewMode = mode;
  }

  getFileData(): FileItem | null {
    return this.selectedItem?.type === "file"
      ? (this.selectedItem.data as FileItem)
      : null;
  }

  getFolderData(): Folder | null {
    return this.selectedItem?.type === "folder"
      ? (this.selectedItem.data as Folder)
      : null;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(["/login"]);
  }
}
