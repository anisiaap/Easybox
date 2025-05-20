import { MD3LightTheme as DefaultTheme } from 'react-native-paper';

export const customTheme = {
    ...DefaultTheme,
    colors: {
        ...DefaultTheme.colors,
        primary: '#883af3',
        secondary: '#03dac6',
        background: '#ffffff',
        surface: '#ffffff',
        text: '#000000',
        // Add more overrides as needed
    },
};