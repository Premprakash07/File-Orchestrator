import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { ModalComponent } from "./components/modals/confirmation-modal/modal.component";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [RouterOutlet, ModalComponent],
  template: `
    <router-outlet></router-outlet>
    <app-modal></app-modal>
  `,
})
export class AppComponent {
  title = "File Storage Application";
}
