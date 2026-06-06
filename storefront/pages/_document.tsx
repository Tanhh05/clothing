import { Html, Head, Main, NextScript } from "next/document";

const title = "TWENTY";
const desc = "TWENTY e-commerce web app built with Next.js.";
const keywords = "TWENTY, Online Shop, E-commerce, NextJS";
const siteUrl = "https://clothing.id.vn";

export default function Document() {
  return (
    <Html>
      <Head>
        <meta content="IE=edge" httpEquiv="X-UA-Compatible" />

        <meta content={desc} name="description" key="description" />
        <meta content={keywords} name="keywords" key="keywords" />

        <meta content="follow, index" name="robots" />
        <meta content="#282828" name="theme-color" />
        <meta content="#282828" name="msapplication-TileColor" />

        <link href="/favicon.ico?v=2" rel="icon" sizes="any" />
        <link href="/favicons/favicon-32x32.png?v=2" rel="icon" type="image/png" />
        <link
          href="/favicons/apple-touch-icon.png?v=2"
          rel="apple-touch-icon"
          sizes="180x180"
        />
        <link
          href="/favicons/favicon-32x32.png?v=2"
          rel="icon"
          sizes="32x32"
          type="image/png"
        />
        <link
          href="/favicons/favicon-16x16.png?v=2"
          rel="icon"
          sizes="16x16"
          type="image/png"
        />
        <link href="/favicon.ico?v=2" rel="shortcut icon" />
        <link href="/favicons/site.webmanifest?v=2" rel="manifest" />

        <meta property="og:url" content={siteUrl} />
        <link rel="canonical" href={siteUrl} />
        <meta property="og:site_name" content="TWENTY" />
        <meta property="og:description" content={desc} key="og_description" />
        <meta property="og:title" content={title} key="og_title" />
        <meta
          property="og:image"
          content={`${siteUrl}/og.png`}
        />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:site" content="@satnaing.dev" />
        <meta name="twitter:title" content={title} key="twitter_title" />
        <meta
          name="twitter:description"
          content={desc}
          key="twitter_description"
        />
        <meta
          name="twitter:image"
          content={`${siteUrl}/og.png`}
        />

        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="black" />
      </Head>
      <body>
        <Main />
        <NextScript />
      </body>
    </Html>
  );
}
