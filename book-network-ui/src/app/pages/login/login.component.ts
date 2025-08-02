import { AuthenticationResponse } from './../../services/models/authentication-response';
import { Component } from '@angular/core';
import { AuthenticationRequest } from '../../services/models';
import { FormsModule } from '@angular/forms';
import { Route, Router } from '@angular/router';
import { AuthenticationService } from '../../services/services';
import { CommonModule } from '@angular/common';
import { TokenService } from '../../services/token/token.service';

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
    private tokenService: TokenService,
  ){}

  login():void{
    this.errorMsg=[];
    this.authService.authenticate({
      body: this.authRequest
    }).subscribe({
      next: (res:AuthenticationResponse):void =>{
        this.tokenService.token = res.token as string;

        this.router.navigate(['books']);
      },
      error : (err):void =>{
        console.log(err);
        if(err.error.validationErrors){
          this.errorMsg = err.error.validationErrors;
        }else{
          this.errorMsg.push(err.error.error);
        }
      }
    });
  }

  register():void{
    this.router.navigate(['register'])
  }

}
