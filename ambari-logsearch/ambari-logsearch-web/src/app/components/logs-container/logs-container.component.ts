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

import {Component} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/takeUntil';
import {FilteringService} from '@app/services/filtering.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {AuditLog} from '@app/classes/models/audit-log';
import {ServiceLog} from '@app/classes/models/service-log';
import {LogField} from '@app/classes/models/log-field';
import {Tab} from '@app/classes/models/tab';
import {BarGraph} from '@app/classes/models/bar-graph';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry';
import {HistogramOptions} from '@app/classes/histogram-options';
import {ListItem} from '@app/classes/list-item';

@Component({
  selector: 'logs-container',
  templateUrl: './logs-container.component.html',
  styleUrls: ['./logs-container.component.less']
})
export class LogsContainerComponent {

  constructor(private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private appState: AppStateService, private tabsStorage: TabsService, private filtering: FilteringService, private logsContainer: LogsContainerService) {
    this.logsContainer.loadColumnsNames();
    this.logsTypeChange.first().subscribe(() => this.logsContainer.loadLogs());
    appState.getParameter('activeLogsType').subscribe((value: string): void => {
      this.logsType = value;
      this.logsTypeChange.next();
      const fieldsModel = this.logsTypeMapObject.fieldsModel,
        logsModel = this.logsTypeMapObject.logsModel;
      this.availableColumns = fieldsModel.getAll().takeUntil(this.logsTypeChange).map((fields: LogField[]): ListItem[] => {
        return fields.filter((field: LogField): boolean => field.isAvailable).map((field: LogField): ListItem => {
          return {
            value: field.name,
            label: field.displayName || field.name,
            isChecked: field.isDisplayed
          };
        });
      });
      fieldsModel.getAll().takeUntil(this.logsTypeChange).subscribe(columns => {
        const availableFields = columns.filter((field: LogField): boolean => field.isAvailable),
          availableNames = availableFields.map((field: LogField): string => field.name);
        if (availableNames.length) {
          this.logs = logsModel.getAll().map((logs: (AuditLog | ServiceLog)[]): (AuditLog | ServiceLog)[] => {
            return logs.map((log: AuditLog | ServiceLog): AuditLog | ServiceLog => {
              return availableNames.reduce((obj, key) => Object.assign(obj, {
                [key]: log[key]
              }), {});
            });
          });
        }
        this.displayedColumns = columns.filter((column: LogField): boolean => column.isAvailable && column.isDisplayed);
      });
    });
    appState.getParameter('activeFiltersForm').subscribe((form: FormGroup): void => {
      this.filtersFormChange.next();
      form.valueChanges.takeUntil(this.filtersFormChange).subscribe(() => this.logsContainer.loadLogs());
      this.filtersForm = form;
    });
    serviceLogsHistogramStorage.getAll().subscribe((data: BarGraph[]): void => {
      this.histogramData = this.logsContainer.getHistogramData(data);
    });
    appState.getParameter('isServiceLogContextView').subscribe((value: boolean) => this.isServiceLogContextView = value);
  }

  tabs: Observable<Tab[]> = this.tabsStorage.getAll();

  filtersForm: FormGroup;

  private logsType: string;

  private filtersFormChange: Subject<any> = new Subject();

  private logsTypeChange: Subject<any> = new Subject();

  get logsTypeMapObject(): any {
    return this.logsContainer.logsTypeMap[this.logsType];
  }

  get totalCount(): number {
    return this.logsContainer.totalCount;
  }

  logs: Observable<AuditLog[] | ServiceLog[]>;

  availableColumns: Observable<LogField[]>;

  displayedColumns: any[] = [];

  histogramData: {[key: string]: number};

  readonly histogramOptions: HistogramOptions = {
    keysWithColors: this.logsContainer.colors
  };

  get autoRefreshRemainingSeconds(): number {
    return this.filtering.autoRefreshRemainingSeconds;
  }

  get autoRefreshMessageParams(): any {
    return {
      remainingSeconds: this.autoRefreshRemainingSeconds
    };
  }

  /**
   * The goal is to provide the single source for the parameters of 'xyz events found' message.
   * @returns {Object}
   */
  get totalEventsFoundMessageParams(): object {
    return {
      totalCount: this.totalCount
    }
  }

  isServiceLogContextView: boolean = false;

  get isServiceLogsFileView(): boolean {
    return this.logsContainer.isServiceLogsFileView;
  }

  get activeLog(): ActiveServiceLogEntry | null {
    return this.logsContainer.activeLog;
  }

  setCustomTimeRange(startTime: number, endTime: number): void {
    this.filtering.setCustomTimeRange(startTime, endTime);
  }

  onSwitchTab(activeTab: Tab): void {
    this.logsContainer.switchTab(activeTab);
  }

  onCloseTab(activeTab: Tab, newActiveTab: Tab): void {
    this.tabsStorage.deleteObjectInstance(activeTab);
    if (newActiveTab) {
      this.onSwitchTab(newActiveTab);
    }
  }
}
