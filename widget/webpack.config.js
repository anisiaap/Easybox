const path = require('path');
const CopyPlugin = require('copy-webpack-plugin');
const Dotenv = require('dotenv-webpack');
const webpack = require('webpack');

module.exports = {
    mode: 'development',  // or 'production'
    entry: './src/index.js',
    output: {
        filename: 'easybox-widget.bundle.js',
        path: path.resolve(__dirname, 'dist'),
        library: 'EasyboxWidget',
        libraryTarget: 'umd',
    },
    module: {
        rules: [
            {
                test: /\.(js|jsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: [
                            '@babel/preset-env',
                            '@babel/preset-react'
                        ]
                    }
                }
            },
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            },
            {
                test: /\.(png|jpe?g|gif|svg)$/,
                type: 'asset/resource'
            }
        ]
    },
    resolve: {
        extensions: ['.js', '.jsx']
    },
    plugins: [
        new Dotenv({
            path: `./.env.${process.env.NODE_ENV}` // will load .env.production or .env.development
        }),
        new webpack.DefinePlugin({
            __REACT_APP_API_URL__: JSON.stringify(process.env.REACT_APP_API_URL)
        }),
        new CopyPlugin({
            patterns: [
                { from: 'public/index.html', to: '.' }
            ]
        })
    ]
};
