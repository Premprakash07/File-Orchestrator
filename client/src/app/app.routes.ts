import { Routes } from "@angular/router";
import { LoginComponent } from "./components/login/login.component";
import { SignupComponent } from "./components/signup/signup.component";
import { FileManagerComponent } from "./components/file-manager/file-manager.component";
import { authGuard } from "./guards/auth.guard";

export const routes: Routes = [
  { path: "", redirectTo: "/login", pathMatch: "full" },
  { path: "login", component: LoginComponent },
  { path: "signup", component: SignupComponent },
  { path: "files", component: FileManagerComponent, canActivate: [authGuard] },
  { path: "**", redirectTo: "/login" },
];
