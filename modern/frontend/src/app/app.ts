import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Application shell: same navbar/footer markup and classes as the AngularJS
 * index.html — stage 5 proves equivalence, it does not redesign. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
})
export class App {}
