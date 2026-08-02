import { Pipe, PipeTransform } from "@angular/core";
import stringsData from "../../assets/strings.json";

@Pipe({
  name: "translate",
  standalone: true,
})
export class TranslatePipe implements PipeTransform {
  private strings: any = stringsData;

  transform(path: string): string {
    if (!path) return "";

    const keys = path.split(".");
    let value: any = this.strings;

    for (const key of keys) {
      if (value && typeof value === "object" && key in value) {
        value = value[key];
      } else {
        return path; // Return the path itself if not found
      }
    }

    return typeof value === "string" ? value : path;
  }
}
