// Tour shape from the backend API.
export interface Tour {
    id?: number;
    name: string;
    description: string;
    origin: string;
    destination: string;
    transportType: string;
    distance: number;
    estimatedTime: number;
    imagePath?: string | null;
    popularity?: number;       // computed on server from log count
    childFriendliness?: number; // computed on server from logs
}
