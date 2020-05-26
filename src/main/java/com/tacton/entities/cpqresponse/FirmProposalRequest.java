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

import java.util.Date;

public class FirmProposalRequest extends CpqEntity {
    private String id;
    private String name;

    private Date modifiedTime;

    private String state;
    private String stateTitle;
    private long itemCount;
    private String cartName;
    private String solution;
    private String fileName;
    private String currency;

    public String getID() { return id; }
    public void setID(String value) { this.id = value; }

    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    public Date getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(Date value) { this.modifiedTime = value; }

    public String getState() { return state; }
    public void setState(String value) { this.state = value; }

    public String getStateTitle() { return stateTitle; }
    public void setStateTitle(String value) { this.stateTitle = value; }

    public long getItemCount() { return itemCount; }
    public void setItemCount(long value) { this.itemCount = value; }

    public String getCartName() {
        return cartName;
    }

    public void setCartName(String cartName) {
        this.cartName = cartName;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.id.equalsIgnoreCase(((FirmProposalRequest) obj).getID());
    }


    @Override
    public String toString() {
        return "FirmProposalRequest{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", modifiedTime=" + modifiedTime +
                ", state='" + state + '\'' +
                ", stateTitle='" + stateTitle + '\'' +
                ", itemCount=" + itemCount +
                ", cartName='" + cartName + '\'' +
                ", solution=" + solution +
                '}';
    }
}