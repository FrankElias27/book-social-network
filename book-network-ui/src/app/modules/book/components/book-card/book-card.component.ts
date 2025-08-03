import { Component, Input } from '@angular/core';
import { BookResponse } from '../../../../services/models';

@Component({
  selector: 'app-book-card',
  standalone: true,
  imports: [],
  templateUrl: './book-card.component.html',
  styleUrl: './book-card.component.scss'
})
export class BookCardComponent {
  private _book: BookResponse = {};
  private _bookCover: string | undefined;

  get bookCover(): string | undefined {
    if (this._book.cover) {
      return 'data:image/jpg;base64,' + this._book.cover
    }
    return 'https://source.unsplash.com/user/c_v_r/1900x800';
  }

  get book():BookResponse{
    return this._book;
  }

  @Input()
  set book(value:BookResponse){
    this._book=value;
  }

}
