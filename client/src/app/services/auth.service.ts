import { Injectable } from "@angular/core";
import { Observable, BehaviorSubject, tap } from "rxjs";
import {
  AuthResponse,
  LoginRequest,
  SignupRequest,
} from "../models/auth.model";
import { ApiService } from "./api.service";

@Injectable({
  providedIn: "root",
})
export class AuthService {
  private tokenKey = "auth_token";
  private userSubject = new BehaviorSubject<any>(null);
  public user$ = this.userSubject.asObservable();

  constructor(private apiService: ApiService) {
    this.loadUser();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>("/auth/login", credentials).pipe(
      tap((response) => {
        this.setToken(response.token);
        this.userSubject.next({
          id: response.id,
          username: response.username,
          email: response.email,
        });
      }),
    );
  }

  signup(userData: SignupRequest): Observable<any> {
    return this.apiService.post("/auth/signup", userData);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem("user");
    this.userSubject.next(null);
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  private loadUser(): void {
    const userStr = localStorage.getItem("user");
    if (userStr) {
      this.userSubject.next(JSON.parse(userStr));
    }
  }

  setUser(user: any): void {
    localStorage.setItem("user", JSON.stringify(user));
    this.userSubject.next(user);
  }
}
