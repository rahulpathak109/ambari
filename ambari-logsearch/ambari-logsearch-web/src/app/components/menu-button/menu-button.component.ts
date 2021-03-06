/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, Input, ViewChild, ElementRef} from '@angular/core';
import {ListItem} from '@app/classes/list-item';
import {ComponentActionsService} from '@app/services/component-actions.service';
import * as $ from 'jquery';

@Component({
  selector: 'menu-button',
  templateUrl: './menu-button.component.html',
  styleUrls: ['./menu-button.component.less']
})
export class MenuButtonComponent {

  constructor(protected actions: ComponentActionsService) {
  }

  @ViewChild('dropdown')
  dropdown: ElementRef;

  @Input()
  label?: string;

  @Input()
  action: string;

  @Input()
  iconClass: string;

  @Input()
  labelClass?: string;

  @Input()
  subItems?: ListItem[];

  @Input()
  isMultipleChoice: boolean = false;

  @Input()
  hideCaret: boolean = false;

  @Input()
  isRightAlign: boolean = false;

  @Input()
  additionalLabelComponentSetter?: string;

  @Input()
  badge: string;

  get hasSubItems(): boolean {
    return Boolean(this.subItems && this.subItems.length);
  }

  get hasCaret(): boolean {
    return this.hasSubItems && !this.hideCaret;
  }

  private clickStartTime: number;

  private readonly longClickInterval = 1000;

  onMouseDown(event: MouseEvent): void {
    if (this.action && event.button === 0) {
      this.clickStartTime = (new Date()).getTime();
    }
  }

  onMouseUp(event: MouseEvent): void {
    if (event.button === 0) {
      const clickEndTime = (new Date()).getTime();
      if (this.hasSubItems && (!this.action || clickEndTime - this.clickStartTime >= this.longClickInterval)) {
        $(this.dropdown.nativeElement).toggleClass('open');
      } else if (this.action) {
        this.actions[this.action]();
      }
      event.stopPropagation();
    }
  }

  updateValue(options: ListItem) {
    // TODO implement value change behaviour
  }

}
