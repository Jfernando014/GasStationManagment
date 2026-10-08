import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="home-placeholder">
      <img src="https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif" alt="Funny cat working" class="funny-img">
      <h2>¡Módulo Principal en Producción!</h2>
      <p>Trabajando en ello :D</p>
    </div>
  `,
  styles: [`
    .home-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      min-height: 500px;
      text-align: center;
      color: var(--text-color-secondary, #6c757d);
    }
    .funny-img {
      max-width: 300px;
      border-radius: 12px;
      margin-bottom: 2rem;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
    h2 {
      color: var(--text-color, #212529);
      margin-bottom: 0.5rem;
    }
  `]
})
export class HomePageComponent { }
