import { AuthenticationResponse } from './../../services/models/authentication-response';
import { Component } from '@angular/core';
import { AuthenticationRequest } from '../../services/models';
import { FormsModule } from '@angular/forms';
import { Route, Router } from '@angular/router';
import { AuthenticationService } from '../../services/services';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
     CommonModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  authRequest: AuthenticationRequest ={ email:'',password:''};
  errorMsg: Array<string>=[];
  constructor(
    private router:Router,
    private authService: AuthenticationService,
  ){}

  login():void{
    this.errorMsg=[];
    this.authService.authenticate({
      body: this.authRequest
    }).subscribe({
      next: (res:AuthenticationResponse):void =>{
        this.router.navigate(['books']);
      },
      error : (err):void =>{
        console.log(err);
      }
    });
  }

  register():void{
    this.router.navigate(['register'])
  }

}
