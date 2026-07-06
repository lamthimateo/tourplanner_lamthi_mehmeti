// MVVM: tour form state + validation between View and Model.
import { Tour } from '../models/tour';

export class TourViewModel {
  id?: number;
  name: string = '';
  description: string = '';
  origin: string = '';
  destination: string = '';
  transportType: string = 'car';
  distance: number = 0;
  estimatedTime: number = 0;
  imagePath: string | null = null;
  popularity?: number;
  childFriendliness?: number;
  submitted: boolean = false; // show validation errors after first save attempt

  static from(tour: Tour): TourViewModel {
    const vm = new TourViewModel();
    vm.id = tour.id;
    vm.name = tour.name;
    vm.description = tour.description ?? '';
    vm.origin = tour.origin;
    vm.destination = tour.destination;
    vm.transportType = tour.transportType;
    vm.distance = tour.distance ?? 0;
    vm.estimatedTime = tour.estimatedTime ?? 0;
    vm.imagePath = tour.imagePath ?? null;
    vm.popularity = tour.popularity;
    vm.childFriendliness = tour.childFriendliness;
    return vm;
  }

  isValid(): boolean {
    return (
      this.name.trim().length > 0 &&
      this.origin.trim().length > 0 &&
      this.destination.trim().length > 0 &&
      this.transportType.trim().length > 0
    );
  }

  toTour(): Tour {
    return {
      id: this.id,
      name: this.name,
      description: this.description,
      origin: this.origin,
      destination: this.destination,
      transportType: this.transportType,
      distance: this.distance,
      estimatedTime: this.estimatedTime,
      imagePath: this.imagePath,
    };
  }
}
