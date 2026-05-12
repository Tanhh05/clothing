import React, { useState, useEffect } from "react";
import { GetStaticProps } from "next";
import Image from "next/image";
import { useTranslations } from "next-intl";
import axios from "axios";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import Button from "../components/Buttons/Button";
import Slideshow from "../components/HeroSection/Slideshow";
import OverlayContainer from "../components/OverlayContainer/OverlayContainer";
import Card from "../components/Card/Card";
import TestiSlider from "../components/TestiSlider/TestiSlider";
import { itemType } from "../context/cart/cart-types";
import LinkButton from "../components/Buttons/LinkButton";
import { mapApiProductToItem } from "../context/Util/productMapper";

// /bg-img/ourshop.png
import ourShop from "../public/bg-img/ourshop.png";

type Props = {
  featuredProducts: itemType[];
  bestSellingProducts: itemType[];
  testimonials: TestimonialItem[];
};

type TestimonialItem = {
  speech: string;
  name: string;
  occupation: string;
};

const unwrapApiData = <T,>(payload: any): T => {
  if (
    payload &&
    typeof payload === "object" &&
    Object.prototype.hasOwnProperty.call(payload, "data")
  ) {
    return payload.data as T;
  }
  return payload as T;
};

const Home: React.FC<Props> = ({
  featuredProducts,
  bestSellingProducts,
  testimonials,
}) => {
  const t = useTranslations("Index");
  const [currentItems, setCurrentItems] = useState(featuredProducts);
  const [isFetching, setIsFetching] = useState(false);

  useEffect(() => {
    if (!isFetching) return;
    const fetchData = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products?page=${
            Math.floor(currentItems.length / 10)
          }&size=10&sortBy=createdAt&direction=desc`
        );
        const productPage = unwrapApiData<any>(res.data);
        const fetchedProducts = (productPage?.content || []).map(mapApiProductToItem);
        setCurrentItems((products) => [...products, ...fetchedProducts]);
      } catch (error) {
        console.error("Failed to fetch more products:", error);
      } finally {
        setIsFetching(false);
      }
    };
    fetchData();
  }, [isFetching, currentItems.length]);

  const handleSeemore = async (
    e: React.MouseEvent<HTMLButtonElement, MouseEvent>
  ) => {
    e.preventDefault();
    setIsFetching(true);
  };

  return (
    <>
      {/* ===== Header Section ===== */}
      <Header />

      {/* ===== Carousel Section ===== */}
      <Slideshow />

      <main id="main-content" className="-mt-20">
        {/* ===== Category Section ===== */}
        <section className="w-full h-auto py-10 border border-b-2 border-gray100">
          <div className="app-max-width app-x-padding h-full grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="w-full sm:col-span-2 lg:col-span-2">
              <OverlayContainer
                imgSrc="/bg-img/banner_minipage1.jpg"
                imgSrc2="/bg-img/banner_minipage1-tablet.jpg"
                imgAlt="New Arrivals"
              >
                <LinkButton
                  href="/product-category/new-arrivals"
                  extraClass="absolute bottom-10-per sm:right-10-per z-20"
                >
                  {t("new_arrivals")}
                </LinkButton>
              </OverlayContainer>
            </div>
            <div className="w-full">
              <OverlayContainer
                imgSrc="/bg-img/banner_minipage2.jpg"
                imgAlt="Women Collection"
              >
                <LinkButton
                  href="/product-category/women"
                  extraClass="absolute bottom-10-per z-20"
                >
                  {t("women_collection")}
                </LinkButton>
              </OverlayContainer>
            </div>
            <div className="w-full">
              <OverlayContainer
                imgSrc="/bg-img/banner_minipage3.jpg"
                imgAlt="Men Collection"
              >
                <LinkButton
                  href="/product-category/men"
                  extraClass="absolute bottom-10-per z-20"
                >
                  {t("men_collection")}
                </LinkButton>
              </OverlayContainer>
            </div>
          </div>
        </section>

        {/* ===== Best Selling Section ===== */}
        <section className="app-max-width w-full h-full flex flex-col justify-center mt-16 mb-20">
          <div className="flex justify-center">
            <div className="w-3/4 sm:w-1/2 md:w-1/3 text-center mb-8">
              <h2 className="text-3xl mb-4">{t("best_selling")}</h2>
              <span>{t("best_selling_desc")}</span>
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-x-4 lg:gap-x-12 gap-y-6 mb-10 app-x-padding">
            {bestSellingProducts.map((item) => (
              <Card key={item.id} item={item} enableVariantDialog />
            ))}
          </div>
        </section>

        {/* ===== Testimonial Section ===== */}
        <section className="w-full hidden h-full py-16 md:flex flex-col items-center bg-lightgreen">
          <h2 className="text-3xl">{t("testimonial")}</h2>
          <TestiSlider items={testimonials} />
        </section>

        {/* ===== Featured Products Section ===== */}
        <section className="app-max-width app-x-padding my-16 flex flex-col">
          <div className="text-center mb-6">
            <h2 className="text-3xl">{t("featured_products")}</h2>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-x-4 gap-y-10 sm:gap-y-6 mb-10">
            {currentItems.map((item) => (
              <Card key={item.id} item={item} enableVariantDialog />
            ))}
          </div>
          <div className="flex justify-center">
            <Button
              value={!isFetching ? t("see_more") : t("loading")}
              onClick={handleSeemore}
            />
          </div>
        </section>

        <div className="border-gray100 border-b-2"></div>

        {/* ===== Our Shop Section */}
        <section className="app-max-width mt-16 mb-20 flex flex-col justify-center items-center text-center">
          <div className="textBox w-3/4 md:w-2/4 lg:w-2/5 mb-6">
            <h2 className="text-3xl mb-6">{t("our_shop")}</h2>
            <span className="w-full">{t("our_shop_desc")}</span>
          </div>
          <div className="w-full app-x-padding flex justify-center">
            <Image src={ourShop} alt="Our Shop" />
          </div>
        </section>
      </main>

      {/* ===== Footer Section ===== */}
      <Footer />
    </>
  );
};

export const getStaticProps: GetStaticProps = async ({ locale }) => {
  let featuredProducts: itemType[] = [];
  let bestSellingProducts: itemType[] = [];
  let testimonials: TestimonialItem[] = [];
  const headers = {
    "Accept-Language": locale || "vi",
    "X-Currency": "VND",
  };
  try {
    const [featuredRes, bestSellingRes] = await Promise.all([
      axios.get(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products?page=0&size=10&sortBy=createdAt&direction=desc`,
        { headers }
      ),
      axios.get(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products?page=0&size=4&sortBy=createdAt&direction=desc`,
        { headers }
      )
    ]);

    const featuredPage = unwrapApiData<any>(featuredRes.data);
    const bestSellingPage = unwrapApiData<any>(bestSellingRes.data);

    featuredProducts = (featuredPage?.content || []).map(mapApiProductToItem);
    bestSellingProducts = (bestSellingPage?.content || []).map(
      mapApiProductToItem
    );
  } catch (error) {
    console.error("getStaticProps(products) failed:", error);
  }

  try {
    const reviewRes = await axios.get(
      `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/reviews/latest?limit=5`,
      { headers }
    );
    const reviews = unwrapApiData<any[]>(reviewRes.data) || [];
    testimonials = reviews
      .filter((review) => review && typeof review === "object")
      .map((review) => ({
        speech:
          typeof review.comment === "string" && review.comment.trim()
            ? review.comment.trim()
            : "Khách hàng hài lòng với sản phẩm.",
        name:
          typeof review.username === "string" && review.username.trim()
            ? review.username.trim()
            : "Customer",
        occupation: `${Math.max(1, Math.min(5, Number(review.rating) || 5))}★`,
      }));
  } catch (error) {
    console.error("getStaticProps(reviews) failed:", error);
  }

  return {
    props: {
      messages: {
        // ...require(`../messages/index/${locale}.json`),
        ...require(`../messages/common/${locale}.json`),
      },
      featuredProducts,
      bestSellingProducts,
      testimonials,
    }, // will be passed to the page component as props
  };
};

export default Home;
