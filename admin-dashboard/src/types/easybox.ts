// types/easybox.ts

export interface Easybox {
    id: number;
    address: string;
    status: string;
    latitude: number;
    longitude: number;
}

export interface EasyboxDto extends Easybox {
    available: boolean;
    distance?: number;
    recommended?: boolean;
}