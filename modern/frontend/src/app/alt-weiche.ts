import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Catch-all route during the Strangler-Fig hybrid phase: every path this app
 * does not own yet belongs to the AngularJS UI, which still serves it at
 * /alt.html#!<path>. Full page load on purpose — the two frameworks never
 * share a document (ADR-0009). Deleted with the last ported route.
 */
@Component({
  selector: 'app-alt-weiche',
  template: '',
})
export class AltWeiche implements OnInit {
  private readonly router = inject(Router);

  ngOnInit(): void {
    window.location.replace('/alt.html#!' + this.router.url);
  }
}
