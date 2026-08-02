import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";

export interface ApiRequestOptions {
  headers?: HttpHeaders | { [header: string]: string | string[] };
  params?: HttpParams | { [param: string]: string | string[] };
  observe?: "body";
  reportProgress?: boolean;
  withCredentials?: boolean;
}

@Injectable({
  providedIn: "root",
})
export class ApiService {
  private baseUrl = "http://localhost:8080/api";

  constructor(private http: HttpClient) {}

  /**
   * GET request
   */
  get<T>(endpoint: string, options?: ApiRequestOptions): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.get<T>(url, options);
  }

  /**
   * POST request
   */
  post<T>(
    endpoint: string,
    body: any,
    options?: ApiRequestOptions,
  ): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.post<T>(url, body, options);
  }

  /**
   * PUT request
   */
  put<T>(
    endpoint: string,
    body: any,
    options?: ApiRequestOptions,
  ): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.put<T>(url, body, options);
  }

  /**
   * DELETE request
   */
  delete<T>(endpoint: string, options?: ApiRequestOptions): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.delete<T>(url, options);
  }

  /**
   * PATCH request
   */
  patch<T>(
    endpoint: string,
    body: any,
    options?: ApiRequestOptions,
  ): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.patch<T>(url, body, options);
  }

  /**
   * Download file (returns Blob)
   */
  downloadFile(
    endpoint: string,
    options?: ApiRequestOptions,
  ): Observable<Blob> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.get(url, {
      ...options,
      responseType: "blob",
    });
  }

  /**
   * Upload file with FormData
   */
  uploadFile<T>(
    endpoint: string,
    formData: FormData,
    options?: ApiRequestOptions,
  ): Observable<T> {
    const url = `${this.baseUrl}${endpoint}`;
    return this.http.post<T>(url, formData, options);
  }
}
