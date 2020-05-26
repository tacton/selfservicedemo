/*
 * The MIT License
 *
 * Copyright 2020 Tacton Systems AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tacton.entities.cpqresponse;

public class Account {

    private String name;
    private String accountId;
    private String accountNumber;

    private String isFrameAgreement;
    private String isGPO;
    private String isReseller;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIsFrameAgreement() {
        return isFrameAgreement;
    }

    public void setIsFrameAgreement(String isFrameAgreement) {
        this.isFrameAgreement = isFrameAgreement;
    }

    public String getIsGPO() {
        return isGPO;
    }

    public void setIsGPO(String isGPO) {
        this.isGPO = isGPO;
    }

    public String getIsReseller() {
        return isReseller;
    }

    public void setIsReseller(String isReseller) {
        this.isReseller = isReseller;
    }


    @Override
    public String toString() {
        return "Account{" +
                "name='" + name + '\'' +
                ", accountId='" + accountId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", isFrameAgreement='" + isFrameAgreement + '\'' +
                ", isGPO='" + isGPO + '\'' +
                ", isReseller='" + isReseller + '\'' +
                '}';
    }
}
