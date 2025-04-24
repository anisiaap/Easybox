const path = require('path');
const CopyPlugin = require('copy-webpack-plugin');


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
            // For CSS:
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            },
            // For images:
            {
                test: /\.(png|jpe?g|gif|svg)$/,
                type: 'asset/resource'
            }
        ]
    },
    devServer: {
        port: 3030,
        host: '0.0.0.0',
        allowedHosts: 'all',  // 👈 this line allows widget.local.easybox
        https: false,
        static: {
            directory: path.join(__dirname, 'public'),
        },
        open: true
    }
    ,
    resolve: {
        extensions: ['.js', '.jsx']
    }
};
