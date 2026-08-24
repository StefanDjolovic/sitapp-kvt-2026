import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { DEVELOPMENT_USER_NAME } from './core/development-user';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly title = 'SitApp';
  protected readonly developmentUserName = DEVELOPMENT_USER_NAME;
}
