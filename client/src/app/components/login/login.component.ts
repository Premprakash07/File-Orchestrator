import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../services/auth.service";
import { TranslatePipe } from "../../pipes/translate.pipe";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = "";
  private translate = new TranslatePipe();

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      username: ["", Validators.required],
      password: ["", Validators.required],
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.authService.login(this.loginForm.value).subscribe({
        next: (response) => {
          this.authService.setUser({
            id: response.id,
            username: response.username,
            email: response.email,
          });
          this.router.navigate(["/files"]);
        },
        error: (error) => {
          this.errorMessage = this.translate.transform(
            "auth.login.invalidCredentials",
          );
        },
      });
    }
  }

  goToSignup(): void {
    this.router.navigate(["/signup"]);
  }
}
