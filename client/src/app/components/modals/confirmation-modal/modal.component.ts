import { Component, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Subject, takeUntil } from "rxjs";
import { ModalService } from "../../../services/modal.service";

export interface ModalConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: "confirm" | "alert";
  buttonAction?: "primary" | "danger" | "warning";
}

@Component({
  selector: "app-modal",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./modal.component.html",
  styleUrls: ["./modal.component.css"],
})
export class ModalComponent implements OnInit, OnDestroy {
  isVisible = false;
  config: ModalConfig = {
    title: "",
    message: "",
    confirmText: "OK",
    cancelText: "Cancel",
    type: "confirm",
    buttonAction: "primary",
  };
  private resolvePromise?: (value: boolean) => void;
  private destroy$ = new Subject<void>();

  constructor(private modalService: ModalService) {}

  ngOnInit(): void {
    this.modalService
      .getModalRequest$()
      .pipe(takeUntil(this.destroy$))
      .subscribe((request) => {
        if (request) {
          this.config = {
            confirmText: "OK",
            cancelText: "Cancel",
            type: "confirm",
            buttonAction: "primary",
            ...request.config,
          };
          this.resolvePromise = request.resolve;
          this.isVisible = true;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  confirm(): void {
    this.isVisible = false;
    if (this.resolvePromise) {
      this.resolvePromise(true);
    }
  }

  cancel(): void {
    this.isVisible = false;
    if (this.resolvePromise) {
      this.resolvePromise(false);
    }
  }

  close(): void {
    this.cancel();
  }
}
