# traQraft - traQ 連携マイクラプラグイン

**&copy; 2025 traP Community**  
License: [MIT License](LICENSE)

Minecraft アカウントと traQ アカウントを連携できるようにする PaperMC サーバー用プラグインです。

> [!Important]
> 現時点では PaperMC 1.21.4 のみをサポートしています。

## 機能

- このプラグインが有効化されたときに自動でホワイトリストを有効化します。
- ホワイトリストでキックされたときに、traQ 連携用の合言葉を発行してプレイヤーに表示します。
- プレイヤーが traQ の指定されたチャンネルに合言葉を送信することで、Minecraft アカウントと traQ アカウントが連携され、ホワイトリストに追加されます。

## 実装予定

- traQ の指定されたチャンネルのチャットと Minecraft 内のチャットを連携させる機能
- traQ の指定されたチャンネルのトピックに Minecraft 内の情報を反映させる機能

## 使い方

> [!Tip]
> PaperMC サーバーの起動方法はここでは説明しません。

1. traQ BOT を用意します。
    - 動作モードは HTTP にしてください。WebSocket には対応していません。
    - エンドポイントはポート開放されている必要があります。例えば `traqraft.xgames.jp` というサーバーアドレスで Minecraft サーバーを建てていて `30001` 番をポート解放した場合は、`https://traqraft.xgames.jp:30001/` を指定してください。XServer GAMEs を利用している場合、解放できるポート番号が限られていることに注意してください。
    - `MESSAGE_CREATED` イベントを購読してください。
    - 4 で指定するチャンネルに参加させてください。
2. [最新版の jar ファイルをダウンロード](https://github.com/traP-jp/traQraft/releases/latest) します。
3. PaperMC サーバーの `plugins/` フォルダにダウンロードした jar ファイルを配置します。
4. `plugins/traQraft/config.yml` を作成し、以下の内容を記述します。
    ```yaml
    traQ:
      port: 30001 # traQ BOT のエンドポイントのポート番号
      verificationToken: "" # traQ BOT の Verification Token
      botAccessToken: "" # traQ BOT の Access Token
      channelIds:
        link: "" # Minecraft アカウントと traQ アカウントを連携するためのチャンネルの UUID
        chat: "" # Minecraft のチャットと連動させるチャンネルの UUID
    ```
    ここで指定するチャンネルには、BOT を参加させてください。
5. サーバーを起動するか、`reload confirm` コマンドを実行します。
    - エラーが出ずに、サーバーログに `[traQraft] traQraft Enabled!` と表示されれば成功です。
