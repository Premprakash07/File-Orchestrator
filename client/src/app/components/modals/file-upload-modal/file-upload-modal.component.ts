import { Component, EventEmitter, Output, Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { TranslatePipe } from "../../../pipes/translate.pipe";
import { FileItem } from "../../../models/file.model";

export interface FileUploadOptions {
  file: File;
  aiSummarize: boolean;
  extractMetadata: boolean;
  generateThumbnail: boolean;
  tags: string;
}

@Component({
  selector: "app-file-upload-modal",
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: "./file-upload-modal.component.html",
  styleUrls: ["./file-upload-modal.component.css"],
})
export class FileUploadModalComponent {
  @Output() upload = new EventEmitter<FileUploadOptions>();
  @Output() cancel = new EventEmitter<void>();
  @Input() existingFiles: FileItem[] = [];

  isVisible = false;
  selectedFile: File | null = null;
  isDuplicate = false;
  aiSummarize = false;
  extractMetadata = false;
  generateThumbnail = false;
  tags = "";

  open(file: File): void {
    this.selectedFile = file;
    this.aiSummarize = false;
    this.extractMetadata = false;
    this.generateThumbnail = false;
    this.tags = "";

    // Check for duplicate file
    this.isDuplicate = this.existingFiles.some(
      (existingFile) =>
        existingFile.fileName === file.name &&
        existingFile.fileType === file.type,
    );

    this.isVisible = true;
  }

  close(): void {
    this.isVisible = false;
    this.selectedFile = null;
    this.cancel.emit();
  }

  confirmUpload(): void {
    if (this.selectedFile) {
      this.upload.emit({
        file: this.selectedFile,
        aiSummarize: this.aiSummarize,
        extractMetadata: this.extractMetadata,
        generateThumbnail: this.generateThumbnail,
        tags: this.tags,
      });
      this.isVisible = false;
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  }

  getFileExtension(): string {
    if (!this.selectedFile) return "";
    const name = this.selectedFile.name;
    return name.substring(name.lastIndexOf(".") + 1).toUpperCase();
  }
}
