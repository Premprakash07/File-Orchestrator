import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { ModalConfig } from "../components/modals/confirmation-modal/modal.component";

export interface ModalRequest {
  config: ModalConfig;
  resolve: (value: boolean) => void;
}

@Injectable({
  providedIn: "root",
})
export class ModalService {
  private modalRequest$ = new BehaviorSubject<ModalRequest | null>(null);

  getModalRequest$() {
    return this.modalRequest$.asObservable();
  }

  confirm(
    title: string,
    message: string,
    confirmText = "OK",
    cancelText = "Cancel",
    buttonAction: "primary" | "danger" | "warning" = "primary",
  ): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.modalRequest$.next({
        config: {
          title,
          message,
          confirmText,
          cancelText,
          type: "confirm",
          buttonAction,
        },
        resolve,
      });
    });
  }

  alert(
    title: string,
    message: string,
    confirmText = "OK",
    buttonAction: "primary" | "danger" | "warning" = "primary",
  ): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.modalRequest$.next({
        config: {
          title,
          message,
          confirmText,
          type: "alert",
          buttonAction,
        },
        resolve,
      });
    });
  }
}
