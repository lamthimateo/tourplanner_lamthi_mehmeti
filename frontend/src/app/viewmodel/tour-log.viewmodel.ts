// MVVM: log form state + validation between View and Model.
import { TourLog } from '../models/tour-log';

export class TourLogViewModel {
  id?: number;
  logDate: string = new Date().toISOString().slice(0, 10);
  comment: string = '';
  difficulty: number = 1;
  totalDistance: number = 0;
  totalTimeMinutes: number = 0;
  rating: number = 5;
  logDetails: string = '';
  submitted: boolean = false;

  static from(log: TourLog): TourLogViewModel {
    const vm = new TourLogViewModel();
    vm.id = log.id;
    vm.logDate = log.logDate;
    vm.comment = log.comment;
    vm.difficulty = log.difficulty;
    vm.totalDistance = log.totalDistance;
    vm.totalTimeMinutes = log.totalTimeMinutes;
    vm.rating = log.rating;
    vm.logDetails = log.logDetails ?? '';
    return vm;
  }

  isValid(): boolean {
    return (
      this.logDate.length > 0 &&
      this.comment.trim().length > 0 &&
      this.difficulty >= 1 && this.difficulty <= 5 &&
      this.rating >= 0 && this.rating <= 10
    );
  }

  toLog(): TourLog {
    return {
      id: this.id,
      logDate: this.logDate,
      comment: this.comment,
      difficulty: this.difficulty,
      totalDistance: this.totalDistance,
      totalTimeMinutes: this.totalTimeMinutes,
      rating: this.rating,
      logDetails: this.logDetails.trim() ? this.logDetails : undefined,
    };
  }
}
